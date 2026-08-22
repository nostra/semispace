# semispace

SemiSpace is a lightweight Open Source interpretation of Tuple Space / Object Space based on ideas from JavaSpaces. 

## Maven wrapepr

Latest versions:
https://central.sonatype.com/artifact/org.apache.maven/apache-maven/versions

```
./mvnw wrapper:wrapper -Dmaven=3.9.9
```

## Installation

SemiSpace is distributed on Maven Central: https://central.sonatype.com/search?q=semispace-main.semispace.org

### Jackson packages for serialization

Serialization is now performed with [jackson](https://github.com/fasterxml/jackson), and you 
need it as a dependency, if not already included in your existing setup.

```xml
    <dependencies>
        <dependency>
            <groupId>tools.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
    
        <!-- Only needed if serializing to XML -->
        <dependency>
            <groupId>tools.jackson.dataformat</groupId>
            <artifactId>jackson-dataformat-xml</artifactId>
        </dependency>
    </dependencies>
```

### Jitpack (alternative to Maven Central)

You can use https://jitpack.io/#nostra/semispace to fetch the SemiSpace binarier. See
the [JitPack homepage](https://jitpack.io/) for details.

You need to merge the following settings together with your existing maven setup:

```
    <repositories>
        <repository>
            <id>jitpack.io</id>
            <url>https://jitpack.io</url>
        </repository>
    </repositories>

    <dependencies>
        <dependency>
            <groupId>com.github.nostra.semispace</groupId>
            <artifactId>semispace-main</artifactId>
            <version>PR2-SNAPSHOT</version>
        </dependency>
    </dependencies>
``` 
