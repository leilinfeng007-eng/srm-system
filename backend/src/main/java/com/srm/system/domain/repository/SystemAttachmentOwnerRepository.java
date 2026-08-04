package com.srm.system.domain.repository;

public interface SystemAttachmentOwnerRepository {
    record Owner(String username, boolean globalOnly) {}
    java.util.Optional<Owner> findOwner(String ownerType, Long ownerId);
}
