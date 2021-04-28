JAVA_HOME=/usr/lib/jvm/jdk1.8.0_281
PATH=$JAVA_HOME/bin:$PATH
mvn clean verify exec:java -Dexec.mainClass=org.golchin.ontology_visualization.GraphMetricsBenchmark
