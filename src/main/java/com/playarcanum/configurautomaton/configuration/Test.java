package com.playarcanum.configurautomaton.configuration;

public class Test implements IConfiguration{
    @Override
    public IConfiguration create() {
        return new Test();
    }
}
