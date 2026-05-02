/*
 * This source file contains database access contracts for the Inkwell platform.
 * The comments explain what each class or method is responsible for and why it exists in this service.
 */
package com.inkwell.newsletter.repository;

import com.inkwell.newsletter.entity.Campaign;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/* This interface groups campaign repository behavior so the module keeps a clear responsibility. */
public interface CampaignRepository extends JpaRepository<Campaign, UUID> {}
