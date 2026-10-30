package com.backend.bangbangbwa.domain.property.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FilterOptionResponse {

    private List<String> transactionTypes;
    private List<String> roomTypes;
    private List<String> sigungus;

    public static FilterOptionResponse of(
            List<String> transactionTypes,
            List<String> roomTypes,
            List<String> sigungus
    ) {
        return new FilterOptionResponse(transactionTypes, roomTypes, sigungus);
    }
}
