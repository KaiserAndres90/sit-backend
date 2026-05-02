package com.sit.controller;

import com.sit.dto.CommentRequest;
import com.sit.dto.CommentResponse;
import com.sit.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    public ResponseEntity<CommentResponse> create(
            @PathVariable Long ticketId,
            @Valid @RequestBody CommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(ticketId, request));
    }

    @GetMapping
    public ResponseEntity<List<CommentResponse>> list(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.listForTicket(ticketId));
    }
}
