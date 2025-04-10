package com.avpuser.env;

public enum EnvironmentEnum {
    TESTS(EnvironmentConst.TESTS),
    DEV(EnvironmentConst.DEV),
    PRODUCTION(EnvironmentConst.PRODUCTION);

    private final String environment;

    EnvironmentEnum(String environment) {
        this.environment = environment;
    }

    public String getEnvironment() {
        return environment;
    }

    public static EnvironmentEnum fromString(String value) {
        for (EnvironmentEnum env : EnvironmentEnum.values()) {
            if (env.environment.equalsIgnoreCase(value)) {
                return env;
            }
        }
        throw new IllegalArgumentException("Unknown environment: " + value);
    }

    @Override
    public String toString() {
        return environment;
    }
}
