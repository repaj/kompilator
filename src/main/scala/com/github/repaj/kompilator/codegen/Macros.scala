/*
 * MIT License
 *
 * Copyright (c) 2018 Konrad Kleczkowski
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.repaj.kompilator.codegen

import com.github.repaj.kompilator.SymbolTable.ArrayEntry
import com.github.repaj.kompilator.ir.{Constant, Name, Operand}
import com.github.repaj.kompilator.vm._

import scala.collection.mutable

/**
  * An implementation of translation macros.
  *
  * @author Konrad Kleczkowski
  */
trait Macros extends AsmOutput with MemoryManaging {
  /**
    * Emits an instruction that reads integer from standard input.
    *
    * @return the result register
    */
  protected final def get(): Register = {
    val reg = select()
    builder += AsmGet(reg)
    reg
  }

  /**
    * Emits an instruction that causes
    * printing operand to the standard output.
    *
    * @param source an operand to show
    */
  protected final def put(source: Operand): Unit = {
    builder += AsmPut(load(source))
  }

  /**
    * Emits a copying instruction of an operand.
    *
    * @param source an operand to copy
    * @return a register with the copy of the operand
    */
  protected final def copy(source: Operand): Register = {
    val loadedReg = load(source)
    val copyReg = select()
    builder += AsmCopy(copyReg, loadedReg)
    copyReg
  }

  /**
    * Computes the effective address for given operands to register A
    *
    * @param base   the base address
    * @param offset the offset
    */
  protected final def lea(base: Operand, offset: Operand): Unit = {
    val Name(entry: ArrayEntry) = base
    val address = getAddress(base)

    builder.label(s"computing lea $base[$offset]")
    val offsetReg = load(offset)
    val relativeBase = address - entry.startIndex
    val relativeBaseReg = load(Constant(relativeBase.abs))
    builder += AsmCopy(Register.A, offsetReg)
    if (relativeBase < 0) {
      builder += AsmSub(Register.A, relativeBaseReg)
    } else if (relativeBase > 0) {
      builder += AsmAdd(Register.A, relativeBaseReg)
    }
  }

  /**
    * Loads a value from an array.
    *
    * @param base   a base address of an array
    * @param offset an offset
    * @return a register with loaded value
    */
  protected final def loadArray(base: Operand, offset: Operand): Register = {
    val resultReg = select()
    lea(base, offset)
    builder += AsmLoad(resultReg)
    resultReg
  }

  /**
    * Stores a value to an array.
    *
    * @param base   a base address of an array
    * @param offset an offset
    */
  protected final def storeArray(base: Operand, offset: Operand, value: Operand): Unit = {
    val valueReg = load(value)
    lea(base, offset)
    builder += AsmStore(valueReg)
  }


  /**
    * Emits an instruction that adds
    * two operands and stores to a register
    *
    * @param left  the left operand
    * @param right the right operand
    * @return the result register
    */
  protected final def add(left: Operand, right: Operand): Register = {
    val leftReg = load(left)
    val rightReg = load(right)
    val resultReg = select()
    builder += AsmCopy(resultReg, leftReg)
    builder += AsmAdd(resultReg, rightReg)
    resultReg
  }

  /**
    * Emits an instruction that subtracts
    * two operands and stores to a register.
    *
    * @param left  the left operand
    * @param right the right operand
    * @return the result register
    */
  protected final def sub(left: Operand, right: Operand): Register = {
    val leftReg = load(left)
    val rightReg = load(right)
    val resultReg = select()
    builder += AsmCopy(resultReg, leftReg)
    builder += AsmSub(resultReg, rightReg)
    resultReg
  }

  /**
    * Emits jump for `<=`.
    *
    * @param leftOp  the left operand
    * @param rightOp the right operand
    * @param label   the label name
    */
  protected final def jumpLe(leftOp: Operand, rightOp: Operand, label: String): Unit = {
    val left = load(leftOp)
    val right = load(rightOp)
    val cmp = select()
    builder += AsmCopy(cmp, left)
    builder += AsmSub(cmp, right)
    builder.comment(s"-> $label")
    builder += AsmJzero(cmp, label)
  }

  /**
    * Emits jump for `>=`.
    *
    * @param leftOp  the left operand
    * @param rightOp the right operand
    * @param label   the label name
    */
  protected final def jumpGe(leftOp: Operand, rightOp: Operand, label: String): Unit = {
    val left = load(leftOp)
    val right = load(rightOp)
    val cmp = select()
    builder += AsmCopy(cmp, right)
    builder += AsmSub(cmp, left)
    builder.comment(s"-> $label")
    builder += AsmJzero(cmp, label)
  }

  /**
    * Emits jump for `>`.
    *
    * @param leftOp  the left operand
    * @param rightOp the right operand
    * @param label   the label name
    */
  protected final def jumpGt(leftOp: Operand, rightOp: Operand, label: String): Unit = {
    val left = load(leftOp)
    val right = load(rightOp)
    val cmp = select()
    builder += AsmCopy(cmp, right)
    builder += AsmInc(cmp)
    builder += AsmSub(cmp, left)
    builder.comment(s"-> $label")
    builder += AsmJzero(cmp, label)
  }

  /**
    * Emits jump for `<`.
    *
    * @param leftOp  the left operand
    * @param rightOp the right operand
    * @param label   the label name
    */
  protected final def jumpLt(leftOp: Operand, rightOp: Operand, label: String): Unit = {
    val left = load(leftOp)
    val right = load(rightOp)
    val cmp = select()
    builder += AsmCopy(cmp, left)
    builder += AsmInc(cmp)
    builder += AsmSub(cmp, right)
    builder.comment(s"-> $label")
    builder += AsmJzero(cmp, label)
  }

  /**
    * Emits jump for `!=`.
    *
    * @param leftOp  the left operand
    * @param rightOp the right operand
    * @param label   the label name
    */
  protected final def jumpNe(leftOp: Operand, rightOp: Operand, label: String): Unit = {
    val left = load(leftOp)
    val right = load(rightOp)
    val cmp = select()
    builder += AsmCopy(cmp, right)
    builder += AsmInc(cmp)
    builder += AsmSub(cmp, left)
    builder += AsmJzero(cmp, label)
    builder += AsmCopy(cmp, left)
    builder += AsmInc(cmp)
    builder += AsmSub(cmp, right)
    builder.comment(s"-> $label")
    builder += AsmJzero(cmp, label)
  }

  /**
    * Emits multiplication of `left` and `right` that yields to the register.
    *
    * @param left  the left operand
    * @param right the right operand
    * @return the result register
    */
  protected final def longMul(left: Operand, right: Operand): Register = {
    val a = select()
    val b = select()
    val result = select()

    val begin = getLabel("macro.mul.begin")
    val loop = getLabel("macro.mul.loop")
    val rest = getLabel("macro.mul.rest")
    val odd = getLabel("macro.mul.odd")
    val end = getLabel("macro.mul.end")

    builder.label(begin)
    builder += AsmCopy(a, load(left))
    builder += AsmCopy(b, load(right))
    builder += AsmSub(result, result)
    builder.label(loop)
    builder += AsmJzero(b, end)
    builder += AsmJodd(b, odd)
    builder.label(rest)
    builder += AsmAdd(a, a)
    builder += AsmHalf(b)
    builder += AsmJump(loop)
    builder.label(odd)
    builder += AsmAdd(result, a)
    builder += AsmJump(rest)
    builder.label(end)
    result
  }

  /**
    * Emits division of `left` and `right` that yields quotient to the register.
    *
    * @param left  the left operand
    * @param right the right operand
    * @return the result register
    */
  protected final def longDiv(left: Operand, right: Operand): Register = longDiv0(left, right, isRem = false)

  /**
    * Emits division of `left` and `right` that yields reminder to the register.
    *
    * @param left  the left operand
    * @param right the right operand
    * @return the result register
    */
  protected final def longRem(left: Operand, right: Operand): Register = longDiv0(left, right, isRem = true)

  /**
    * Emits generic long binary division algorithm.
    *
    * @param leftOp  the left operand
    * @param rightOp the right operand
    * @param isRem   `true` if reminder should be returned
    * @return a register with quotient or remainder, according to the `isRem`
    */
  private def longDiv0(leftOp: Operand, rightOp: Operand, isRem: Boolean): Register = {
    val left = load(leftOp)
    val right = load(rightOp)

    val dividend = select()
    val divisor = select()
    val quotient = select()
    val k = select()
    val cmp = select()

    val begin = getLabel("macro.div.begin")
    val kIncLoop = getLabel("macro.div.kIncLoop")
    val kDecLoop = getLabel("macro.div.kDecLoop")
    val end = getLabel("macro.div.end")
    val zeroEnd = getLabel("macro.div.zeroEnd")

    builder.label(begin)
    builder += AsmSub(quotient, quotient)
    builder += AsmJzero(right, zeroEnd)
    builder += AsmCopy(divisor, right)
    builder += AsmCopy(dividend, left)
    builder += AsmSub(k, k)
    builder.label(kIncLoop)
    builder += AsmCopy(cmp, dividend)
    builder += AsmInc(cmp)
    builder += AsmSub(cmp, divisor)
    builder += AsmJzero(cmp, kDecLoop)
    builder += AsmAdd(divisor, divisor)
    builder += AsmInc(k)
    builder += AsmJump(kIncLoop)
    builder.label(kDecLoop)
    builder += AsmJzero(k, end)
    builder += AsmDec(k)
    builder += AsmHalf(divisor)
    builder += AsmAdd(quotient, quotient)
    builder += AsmCopy(cmp, dividend)
    builder += AsmInc(cmp)
    builder += AsmSub(cmp, divisor)
    builder += AsmJzero(cmp, kDecLoop)
    builder += AsmSub(dividend, divisor)
    builder += AsmInc(quotient)
    builder += AsmJump(kDecLoop)
    builder.label(zeroEnd)
    builder += AsmSub(dividend, dividend)
    builder.label(end)
    if (isRem) dividend else quotient
  }

  /**
    * Emits a constant to the specified register.
    *
    * @param register the target register
    * @param value    the value to be emitted to the given register
    */
  protected final def emitConstant(register: Register, value: BigInt): Unit = {
    def ones(bigInt: BigInt): Int = (0 until bigInt.bitLength).count(bigInt.testBit)

    builder += AsmSub(register, register)
    if (value <= 5 * value.bitLength + ones(value)) {
      ((1: BigInt) to value).foreach { _ =>
        builder += AsmInc(register)
      }
    } else {
      for (bit <- value.toString(2)) {
        builder += AsmAdd(register, register)
        if (bit == '1') builder += AsmInc(register)
      }
    }
  }

  private def getLabel(prefix: String): String = {
    val count = labelTable.getOrElse(prefix, 0)
    labelTable(prefix) = labelTable.getOrElse(prefix, 0) + 1
    s"$prefix$count"
  }

  private val labelTable = new mutable.HashMap[String, Int]
}