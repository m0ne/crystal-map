package com.couchbase.todolite.model;

import kaufland.com.coachbasebinderapi.CblDefault;
import kaufland.com.coachbasebinderapi.CblEntity;
import kaufland.com.coachbasebinderapi.CblField;

@CblEntity
public class Sub {

    @CblField
    @CblDefault(value = "foo")
    private String test;

}
