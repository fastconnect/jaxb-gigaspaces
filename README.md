jaxb-gigaspaces
===============

The project is an example of XSD to Java 'Space Document compliant' classes generation using JaxB.

First there is the maven code generation plugin (jaxb-gigaspaces-plugin) that is an extention of default JaxB. You have to install it in your repo (mvn install on the project). Note that dependency was gs 8.0.0 (latest release when I actually build this sample). It should work with more recent releases.

Once done, you can look at the demo project that shows how you can generates java classes that are actually Space Documents (pojo that extends space documents and set properties...).

In the pom you have the maven-jaxb2-plugin that uses the jaxb-gigaspaces-plugin (conf below). You can generate classes using mvn generate-sources (generated in target/generated-sources).

The generated classes can be used as space documents.

<plugin>
  			<groupId>org.jvnet.jaxb2.maven2</groupId>
				<artifactId>maven-jaxb2-plugin</artifactId>
				<executions>
					<execution>
						<phase>generate-sources</phase>
						<goals>
							<goal>generate</goal>
						</goals>
					</execution>
				</executions>
				<configuration>
					<generatePackage>fr.fastconnect.xml.jaxb.generated</generatePackage>
					<generateDirectory>${project.build.directory}/generated-sources</generateDirectory>
					<extension>true</extension>
					<args>
						<arg>-XopenspacesDocument</arg>
					</args>
					<plugins>
						<plugin>
							<groupId>fr.fastconnect.gigaspaces.jaxb</groupId>
							<artifactId>jaxb-gigaspaces-plugin</artifactId>
							<version>0.0.1-SNAPSHOT</version>
						</plugin>
					</plugins>
				</configuration>
			</plugin>