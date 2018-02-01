section .text
	global _start

_start:
	mov rbx, 0

beginning:
 	inc rbx
	cmp rbx, 100000
 	jne beginning
 	mov rax,60
	mov rdi,0
	syscall
