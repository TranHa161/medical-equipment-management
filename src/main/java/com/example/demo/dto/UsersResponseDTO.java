package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UsersResponseDTO {
	private Long id;
	private String username;
    private String fullName;
    private String email;
    private String phone;
    private String roleName;
    private Integer roleId;
    private Boolean isActive;
    private String profilePicture;
}
