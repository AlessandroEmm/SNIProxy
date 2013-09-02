import MultiJvmKeys.MultiJvm

multiJvmSettings

inConfig(MultiJvm)(configScalariformSettings)

compileInputs in (MultiJvm, compile) <<= (compileInputs in (MultiJvm, compile)) dependsOn (ScalariformKeys.format in MultiJvm)
