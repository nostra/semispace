# Mutation tests

Run mutation tests with the following. Notice that you might
not want to run all tests.
```
./mvnw -DtargetTests='org.semispace.List*Test' test-compile org.pitest:pitest-maven:mutationCoverage
./mvnw -DtargetTests='org.semispace.take.*Test' test-compile org.pitest:pitest-maven:mutationCoverage
```
Then:
```
open semispace-main/target/pit-reports/index.html
```

