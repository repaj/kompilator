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

import com.github.repaj.kompilator.SymbolTable.VariableEntry
import com.github.repaj.kompilator.vm.Register

/**
  * Base class for descriptors entry.
  */
private[codegen] sealed abstract class DescriptorEntry

/**
  * Descriptor entry that contains variable.
  *
  * @param entry the symbol table entry
  */
private[codegen] final case class DescVar(entry: VariableEntry) extends DescriptorEntry

/**
  * Descriptor entry that contains temporary variable.
  *
  * @param id the id of temporary
  */
private[codegen] final case class DescTemp(id: Int) extends DescriptorEntry

/**
  * Base class for location of descriptor entries.
  */
private[codegen] sealed abstract class EntryLocation

/**
  * Indicates that given variable is at memory.
  *
  * @param address the address
  */
private[codegen] final case class MemoryLocation(address: BigInt) extends EntryLocation

/**
  * Indicates that given variable is in register.
  *
  * @param register the register
  */
private[codegen] final case class RegisterLocation(register: Register) extends EntryLocation
