package com.englishlearning.features.category.service;
public class CategoryFailure extends RuntimeException {
 public enum Reason { INVALID, UNAUTHENTICATED, DENIED, MISSING }
 private final Reason reason;
 public CategoryFailure(Reason reason){this.reason=reason;}
 public Reason reason(){return reason;}
}
