package com.nc.sinpase.poc.modulith.covoit.identity.domain;

import java.util.Optional;

public interface RoleRepository {

    Optional<Role> findByName(String name);

    void save(Role role);
}
