/*******************************************************************************
 * Copyright (c) 2013, Salesforce.com, Inc.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 *     Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *     Neither the name of Salesforce.com nor the names of its contributors may 
 *     be used to endorse or promote products derived from this software without 
 *     specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE 
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL 
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR 
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER 
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, 
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.salesforce.phoenix.expression;

import java.io.*;
import java.util.List;

import org.apache.hadoop.hbase.filter.CompareFilter.CompareOp;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.io.WritableUtils;

import com.salesforce.phoenix.expression.visitor.ExpressionVisitor;
import com.salesforce.phoenix.schema.PDataType;
import com.salesforce.phoenix.schema.tuple.Tuple;
import com.salesforce.phoenix.util.ByteUtil;


/**
 * 
 * Implementation for <,<=,>,>=,=,!= comparison expressions
 * @author jtaylor
 * @since 0.1
 */
public class ComparisonExpression extends BaseCompoundExpression {
    private CompareOp op;
    private static final String[] CompareOpString = new String[CompareOp.values().length];
    static {
        CompareOpString[CompareOp.EQUAL.ordinal()] = " = ";
        CompareOpString[CompareOp.NOT_EQUAL.ordinal()] = " != ";
        CompareOpString[CompareOp.GREATER.ordinal()] = " > ";
        CompareOpString[CompareOp.LESS.ordinal()] = " < ";
        CompareOpString[CompareOp.GREATER_OR_EQUAL.ordinal()] = " >= ";
        CompareOpString[CompareOp.LESS_OR_EQUAL.ordinal()] = " <= ";
    }
    
    public ComparisonExpression() {
    }

    public ComparisonExpression(CompareOp op, List<Expression> children) {
        super(children);
        if (op == null) {
            throw new NullPointerException();
        }
        this.op = op;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + op.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!super.equals(obj)) return false;
        if (getClass() != obj.getClass()) return false;
        ComparisonExpression other = (ComparisonExpression)obj;
        if (op != other.op) return false;
        return true;
    }

    @Override
    public PDataType getDataType() {
        return PDataType.BOOLEAN;
    }

    @Override
    public boolean evaluate(Tuple tuple, ImmutableBytesWritable ptr) {
        if (!children.get(0).evaluate(tuple, ptr)) {
            return false;
        }
        byte[] lhsBytes = ptr.get();
        int lhsOffset = ptr.getOffset();
        int lhsLength = ptr.getLength();
        
        if (!children.get(1).evaluate(tuple, ptr)) {
            return false;
        }
        
        ptr.set(ByteUtil.compare(op, children.get(0).getDataType().compareTo(lhsBytes, lhsOffset, lhsLength, 
                    ptr.get(), ptr.getOffset(), ptr.getLength(), children.get(1).getDataType()))
                ? PDataType.TRUE_BYTES : PDataType.FALSE_BYTES);
        return true;
    }
    
    @Override
    public void readFields(DataInput input) throws IOException {
        op = CompareOp.values()[WritableUtils.readVInt(input)];
        super.readFields(input);
    }

    @Override
    public void write(DataOutput output) throws IOException {
        WritableUtils.writeVInt(output, op.ordinal());
        super.write(output);
    }

    @Override
    public final <T> T accept(ExpressionVisitor<T> visitor) {
        List<T> l = acceptChildren(visitor, visitor.visitEnter(this));
        T t = visitor.visitLeave(this, l);
        if (t == null) {
            t = visitor.defaultReturn(this, l);
        }
        return t;
    }

    public CompareOp getFilterOp() {
        return op;
    }
    
    @Override
    public String toString() {
        return (children.get(0) + CompareOpString[getFilterOp().ordinal()] + children.get(1));
    }
}
