// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.apache.impala.rewrite;

import org.apache.impala.analysis.Analyzer;
import org.apache.impala.analysis.BetweenPredicate;
import org.apache.impala.analysis.BinaryPredicate;
import org.apache.impala.analysis.CompoundPredicate;
import org.apache.impala.analysis.Expr;
import org.apache.impala.analysis.Predicate;
import org.apache.impala.common.AnalysisException;

/**
 * Rewrites BetweenPredicates into an equivalent conjunctive/disjunctive
 * CompoundPredicate.
 * Examples:
 * A BETWEEN X AND Y ==> A >= X AND A <= Y
 * A NOT BETWEEN X AND Y ==> A < X OR A > Y
 */
public class BetweenToCompoundRule implements ExprRewriteRule {
  public static ExprRewriteRule INSTANCE = new BetweenToCompoundRule();

  @Override
  public Expr apply(Expr expr, Analyzer analyzer) throws AnalysisException {
    if (!(expr instanceof BetweenPredicate)) return expr;
    BetweenPredicate bp = (BetweenPredicate) expr;
    Expr result = null;
    if (bp.isNotBetween()) {
      // Rewrite into disjunction.
      Predicate lower = new BinaryPredicate(BinaryPredicate.Operator.LT,
          bp.getChild(0), bp.getChild(1));
      Predicate upper = new BinaryPredicate(BinaryPredicate.Operator.GT,
          bp.getChild(0), bp.getChild(2));
      result = new CompoundPredicate(CompoundPredicate.Operator.OR, lower, upper);
    } else {
      // Rewrite into conjunction.
      Predicate lower = new BinaryPredicate(BinaryPredicate.Operator.GE,
          bp.getChild(0), bp.getChild(1));
      Predicate upper = new BinaryPredicate(BinaryPredicate.Operator.LE,
          bp.getChild(0), bp.getChild(2));
      result = new CompoundPredicate(CompoundPredicate.Operator.AND, lower, upper);
    }
    result.analyze(analyzer);
    return result;
  }

  private BetweenToCompoundRule() {}
}