package cn.edu.tsinghua.tsfile.read.expression.impl;

import cn.edu.tsinghua.tsfile.read.expression.ExpressionType;
import cn.edu.tsinghua.tsfile.read.expression.IUnaryExpression;
import cn.edu.tsinghua.tsfile.read.filter.basic.Filter;


public class GlobalTimeExpression implements IUnaryExpression {
    private Filter filter;

    public GlobalTimeExpression(Filter filter) {
        this.filter = filter;
    }

    public Filter getFilter() {
        return filter;
    }

    public void setFilter(Filter filter) {
        this.filter = filter;
    }

    @Override
    public ExpressionType getType() {
        return ExpressionType.GLOBAL_TIME;
    }

    public String toString() {
        return "[" + this.filter.toString() + "]";
    }
}
