package com.dodam.board.error;

import org.springframework.http.*;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> notFound(NotFoundException e){
        Map<String,Object> b=new HashMap<>(); b.put("error","NOT_FOUND"); b.put("message",e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(b);
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException ex){
        Map<String,Object> b=new HashMap<>(); b.put("error","VALIDATION_ERROR");
        Map<String,String> fields=new HashMap<>();
        for(FieldError fe:ex.getBindingResult().getFieldErrors()){fields.put(fe.getField(), fe.getDefaultMessage());}
        b.put("fields",fields); return ResponseEntity.badRequest().body(b);
    }
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> illegal(IllegalArgumentException ex){
        Map<String,Object> b=new HashMap<>(); b.put("error","INVALID_ARGUMENT"); b.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(b);
    }
}