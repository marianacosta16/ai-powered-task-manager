package com.matriz.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Body of addComment and editComment (CommentWrite in the contract): only the text. The author and the date are not accepted. */
record CommentWriteRequest(
        @NotBlank(message = "Is required.") @Size(max = 2000, message = "Longer than 2000 characters.") String content) {
}
