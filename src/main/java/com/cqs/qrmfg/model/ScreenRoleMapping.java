package com.cqs.qrmfg.model;

import javax.persistence.*;

@Entity
@Table(name = "qrmfg_screen_role_mapping")
public class ScreenRoleMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "screen_role_mapping_seq")
    @SequenceGenerator(name = "screen_role_mapping_seq", sequenceName = "SCREEN_ROLE_MAPPING_SEQ", allocationSize = 1)
    private Long id;

    @Column(nullable = false, length = 255)
    private String route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    public ScreenRoleMapping() {}
    public ScreenRoleMapping(String route, Role role) {
        this.route = route;
        this.role = role;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
} 