/**
 * Workbench read-only aggregation boundary and the stage 0 reference implementation for the
 * controller/application/domain/repository/infrastructure layering.
 *
 * <p>The reference endpoint performs only a read-only database connectivity probe. It owns no
 * business state and must not become a shortcut for cross-domain table access.
 */
package com.srm.workbench;
