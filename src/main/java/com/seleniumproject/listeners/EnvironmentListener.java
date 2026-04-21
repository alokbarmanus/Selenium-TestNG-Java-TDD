package com.seleniumproject.listeners;

import org.testng.ISuite;
import org.testng.ISuiteListener;

public class EnvironmentListener implements ISuiteListener {

    @Override
    public void onStart(ISuite suite) {
        String env = System.getProperty("env", "dev");
        System.out.println("========================================");
        System.out.println("Environment : " + env.toUpperCase());
        System.out.println("Suite       : " + suite.getName());
        System.out.println("========================================");
    }

    @Override
    public void onFinish(ISuite suite) {
    }
}

