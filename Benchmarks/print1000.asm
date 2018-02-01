section .bss
    digitSpace resb 100
    digitSpacePos resb 8
 
section .text
	global _start

_start:
	mov rbx, 0

beginning:
 	inc rbx
	mov rax,rbx
	call _printRAX
 	cmp rbx, 1000
 	jne beginning
 	mov rax,60
	mov rdi,0
	syscall

_printRAX:
    mov rcx, digitSpace
    mov rdx, 10
    mov [rcx], rdx
    inc rcx
    mov [digitSpacePos], rcx
 
_printRAXLoop:
    mov rdx, 0
    mov rcx, 10
    div rcx
    push rax
    add rdx, 48
 
    mov rcx, [digitSpacePos]
    mov [rcx], dl
    inc rcx
    mov [digitSpacePos], rcx
   
    pop rax
    cmp rax, 0
    jne _printRAXLoop
 
_printRAXLoop2:
    mov rcx, [digitSpacePos]
 
    mov rax, 1
    mov rdi, 1
    mov rsi, rcx
    mov rdx, 1
    syscall
 
    mov rcx, [digitSpacePos]
    dec rcx
    mov [digitSpacePos], rcx
 
    cmp rcx, digitSpace
    jge _printRAXLoop2
 
    ret
