# Simul-OS
An simulation of an operating system that manages multiple disks, printers, and users as concurrent processes. Files stored on a disk can be printed on a printer by a user. The goal of the program is to exploit parallelism and keep devices occupied. On running the program, a JavaFX GUI is displayed, showing the current states of all processes.

Input is taken from /inputs from files named 'USER' followed by a number, indicating a user. The commands are issued in the format

.save X
.end
.print X

where anything between a .save and .end is part of file X. Printing is done by writing to /outputs.

# Prerequisites
Java 8

# Usage
Use make to compile and run the program.
