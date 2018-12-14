package org.cloudfoundry.credhub.handler;

import java.util.List;
import java.util.UUID;

import org.cloudfoundry.credhub.request.PermissionOperation;
import org.cloudfoundry.credhub.request.PermissionsRequest;
import org.cloudfoundry.credhub.request.PermissionsV2Request;
import org.cloudfoundry.credhub.view.PermissionsV2View;
import org.cloudfoundry.credhub.view.PermissionsView;

public class SpyPermissionsHandler implements PermissionsHandler {

  private String findByPathAndActorCalledWithPath;
  private String findByPathAndActorCalledWithActor;
  private PermissionsV2View return_findByPathAndActor;

  public void setReturn_findByPathAndActor(final PermissionsV2View return_findByPathAndActor) {
    this.return_findByPathAndActor = return_findByPathAndActor;
  }

  public String getFindByPathAndActorCalledWithPath() {
    return findByPathAndActorCalledWithPath;
  }

  public void setFindByPathAndActorCalledWithPath(final String findByPathAndActorCalledWithPath) {
    this.findByPathAndActorCalledWithPath = findByPathAndActorCalledWithPath;
  }

  public String getFindByPathAndActorCalledWithActor() {
    return findByPathAndActorCalledWithActor;
  }

  public void setFindByPathAndActorCalledWithActor(final String findByPathAndActorCalledWithActor) {
    this.findByPathAndActorCalledWithActor = findByPathAndActorCalledWithActor;
  }

  @Override
  public PermissionsV2View findByPathAndActor(final String path, final String actor) {
    setFindByPathAndActorCalledWithPath(path);
    setFindByPathAndActorCalledWithActor(actor);
    return return_findByPathAndActor;
  }

  @Override
  public PermissionsView getPermissions(final String name) {
    return null;
  }

  @Override
  public void writePermissions(final PermissionsRequest request) {
  }

  @Override
  public void deletePermissionEntry(final String credentialName, final String actor) {
  }

  @Override
  public PermissionsV2View writePermissions(final PermissionsV2Request request) {
    return null;
  }

  @Override
  public PermissionsV2View getPermissions(final UUID guid) {
    return null;
  }

  @Override
  public PermissionsV2View putPermissions(final String guid, final PermissionsV2Request permissionsRequest) {
    return null;
  }

  @Override
  public PermissionsV2View patchPermissions(final String guid, final List<PermissionOperation> operations) {
    return null;
  }

  @Override
  public PermissionsV2View writeV2Permissions(final PermissionsV2Request permissionsRequest) {
    return null;
  }

  @Override
  public PermissionsV2View deletePermissions(final String guid) {
    return null;
  }
}
