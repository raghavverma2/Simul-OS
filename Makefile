CLASSES = \
	src/Main.java \

$all: $(CLASSES)
	javac src/Main.java
	java -cp src Main
