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

import com.github.repaj.kompilator.codegen.analysis.{DataFlowAnalysisResult, DominatorAnalysis, LivenessAnalysis}
import com.github.repaj.kompilator.ir._
import com.github.repaj.kompilator.vm.{AsmBuilder, AsmHalt, AsmJump}

/**
  * An implementation of code generator.
  *
  * @param builder the builder as an output of code
  */
class CodeGenerator(val builder: AsmBuilder)
  extends AsmOutput
    with Macros with MemoryManaging {

  def emit(blocks: BasicBlock*): Unit = {
    livenessAnalysis = LivenessAnalysis(blocks: _*)
    dominatorAnalysis = DominatorAnalysis(blocks: _*)
    blocks.foreach { b =>
      currBlock = b
      emitBlock(b)
    }
  }

  /**
    * Emits a basic block into the builder.
    *
    * @param basicBlock a basic block to emit
    */
  def emitBlock(basicBlock: BasicBlock): Unit = {
    builder.label(basicBlock.name)
    basicBlock.list.foreach { instruction =>
      clearSelection()
      instruction match {
        case instruction: LoadStoreInstruction => emitLoadStore(instruction)
        case instruction: BinaryInstruction => emitBinary(instruction)
        case instruction: BranchInstruction => saveVariables(); emitBranch(instruction); resetRegistersState()
      }
    }
  }

  /**
    * Emits a load/store instruction.
    *
    * @param instruction a load/store instruction
    */
  private def emitLoadStore(instruction: LoadStoreInstruction): Unit = instruction match {
    case Get(destination) => seize(get(), destination)
    case Put(source) => put(source)
    case Move(source: Name, destination) => seize(copy(source), destination)
    case Move(source: Temp, destination) => seize(copy(source), destination)
    case Move(source: Constant, destination) => seize(load(source), destination)
    case IndexedLoad(base, offset, destination) => seize(loadArray(base, offset), destination)
    case IndexedStore(source, base, offset) => storeArray(base, offset, source)
  }

  /**
    * Emits a binary instruction.
    *
    * @param instruction a binary instruction
    */
  private def emitBinary(instruction: BinaryInstruction): Unit = instruction match {
    case Add(left, right, result) => seize(add(left, right), result)
    case Sub(left, right, result) => seize(sub(left, right), result)
    case Mul(left, right, result) => seize(longMul(left, right), result)
    case Div(left, right, result) => seize(longDiv(left, right), result)
    case Rem(left, right, result) => seize(longRem(left, right), result)
  }

  /**
    * Emits a branch instruction.
    *
    * @param instruction a branch instruction.
    */
  private def emitBranch(instruction: BranchInstruction): Unit = instruction match {
    case Jump(block) => builder += AsmJump(block.name)
    case JumpIf(Eq(left, right), ifTrue, ifFalse) => jumpNe(left, right, ifFalse.name); builder += AsmJump(ifTrue.name)
    case JumpIf(Ne(left, right), ifTrue, ifFalse) => jumpNe(left, right, ifTrue.name); builder += AsmJump(ifFalse.name)
    case JumpIf(Le(left, right), ifTrue, ifFalse) => jumpLe(left, right, ifTrue.name); builder += AsmJump(ifFalse.name)
    case JumpIf(Ge(left, right), ifTrue, ifFalse) => jumpGe(left, right, ifTrue.name); builder += AsmJump(ifFalse.name)
    case JumpIf(Lt(left, right), ifTrue, ifFalse) => jumpLt(left, right, ifTrue.name); builder += AsmJump(ifFalse.name)
    case JumpIf(Gt(left, right), ifTrue, ifFalse) => jumpGt(left, right, ifTrue.name); builder += AsmJump(ifFalse.name)
    case Halt => builder += AsmHalt
  }

  /**
    * Returns the liveness analysis for a block.
    */
  def liveness(bb: BasicBlock): DataFlowAnalysisResult[Operand] = livenessAnalysis(bb)

  /**
    * Returns the dominators of `bb`.
    *
    * @param bb a basic block
    * @return a dominator set of the `bb`
    */
  def dom(bb: BasicBlock): Set[BasicBlock] = dominatorAnalysis(bb)

  /**
    * Returns the current block being emitted.
    */
  def currentBlock: BasicBlock = currBlock

  private var currBlock: BasicBlock = _

  private var livenessAnalysis: Map[BasicBlock, DataFlowAnalysisResult[Operand]] = _

  private var dominatorAnalysis: Map[BasicBlock, Set[BasicBlock]] = _
}