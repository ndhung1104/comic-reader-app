package com.group09.ComicReader.creatorrequest.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCreatorRequest {
    @Size(max = 2000)
    private String message;
}
