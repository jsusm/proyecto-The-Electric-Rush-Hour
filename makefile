entrada ?= ./entrada.txt

build:
	javac Main.java

run: build
	java Main $(entrada)

ejemplo0: build
	java Main ./case0.txt

ejemplo1: build
	java Main ./case1.txt

ejemplo2: build
	java Main ./case2.txt

ejemplo3: build
	java Main ./case3.txt

ejemplo4: build
	java Main ./case4.txt

clean:
	rm *.class
