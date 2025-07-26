package com.cqs.qrmfg.model;

import javax.persistence.*;

@Entity
@Table(name = "QRMFG_PERMISSIONS")
public class ScreenRoleMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "permissions_seq")
    @SequenceGenerator(name = "permissions_seq", sequenceName = "QRMFG_PERMISSIONS_SEQ", allocationSize = 1)
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