<!DOCTYPE html>
<!--
Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.

Permission is hereby granted, free of charge, to any person obtaining a copy of this
software and associated documentation files (the "Software"), to deal in the Software
without restriction, including without limitation the rights to use, copy, modify,
merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
-->
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<html lang="en">
  <head>
      <title>AWS SaaS Factory PostgreSQL Row Level Security</title>
      <link href="/webjars/bootstrap/4.5.0/css/bootstrap.min.css" rel="stylesheet">
  </head>
  <body>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li class="breadcrumb-item"><a href="/">Home</a></li>
      <li class="breadcrumb-item active" aria-current="page">Tenant Manager</li>
    </ol>
  </nav>
    <div class="container pt-3">
      <h3>Admin Tenant Management</h3>
      <p>These actions will be executed as the SaaS administrator and not restricted by RLS policies.</p>
      <p>&nbsp;</p>
      <c:if test="${not empty msg}">
        <div class="row">
          <div class="col-12">
            <div class="alert alert-${css} alert-dismissible fade show" role="alert">
              <strong>${msg}</strong>
              <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
            </div>
          </div>
        </div>
      </c:if>
    <div class="row">
      <div class="col-10">
        <h4>Tenants</h4>
      </div>
      <div class="col-2 align-self-center">
        <a role="button" class="btn btn-success float-right" href="/admin/newTenant">Add Tenant</a>
      </div>
    </div>
    <div class="row">
      <table class="table table-hover">
        <thead class="thead-light">
          <tr>
            <th style="width: 35%" scope="col">ID</th>
            <th style="width: 10%" scope="col">Status</th>
            <th style="width: 10%" scope="col">Tier</th>
            <th style="width: 30%" scope="col">Name</th>
            <th style="width: 15%" scope="col"></th>
          </tr>
        </thead>
        <c:forEach items="${tenants}" var="tenant">
          <tr class="clickable-row" data-href="/admin/updateTenant?id=${tenant.id}">
            <th scope="row">${tenant.id}</td>
            <td>${tenant.status}</td>
            <td>${tenant.tier}</td>
            <td>${tenant.name}</td>
            <td><div class="float-right"><a role="button" class="btn btn-info" href="/admin/updateTenant?id=${tenant.id}">Edit</a> <a role="button" class="btn btn-danger" href="/admin/deleteTenant?id=${tenant.id}">Delete</a></div></td>
          </tr>
        </c:forEach>
      </table>
    </div>
  </div>
  <script src="/webjars/jquery/3.5.1/jquery.min.js"></script>
  <script src="/webjars/bootstrap/4.5.0/js/bootstrap.bundle.min.js"></script>
  <script type="text/javascript">
  $(document).ready(function($) {
    $(".clickable-row").click(function() {
      window.location.assign($(this).data("href"));
    });
  });
  </script>
  </body>
</html>