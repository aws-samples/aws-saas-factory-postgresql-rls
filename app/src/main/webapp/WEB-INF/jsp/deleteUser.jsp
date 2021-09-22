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
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html lang="en">
<head>
  <title>Delete User</title>
  <link href="/webjars/bootstrap/4.5.0/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li class="breadcrumb-item"><a href="/">Home</a></li>
      <li class="breadcrumb-item"><a href="/tenant">User Manager</a></li>
      <li class="breadcrumb-item active" aria-current="page">Delete User</li>
    </ol>
  </nav>
<div class="container pt-3">
<h2>Are you sure?</h2>
<c:if test="${not empty msg}">
  <div class="row">
    <div class="col">
      <div class="alert alert-${css} alert-dismissible fade show" role="alert">
        <strong>${msg}</strong>
        <button type="button" class="close" data-dismiss="alert" aria-label="Close"><span aria-hidden="true">&times;</span></button>
      </div>
    </div>
  </div>
</c:if>
  <form:form modelAttribute="user" method="post" action="/tenant/deleteUser">
	<form:hidden path="id" />
	<div class="form-check">
	  <input class="form-check-input" type="checkbox" value="" id="confirm" required />
      <label class="form-check-label" for="confirm">Check the box to confirm removal of user ${user.email}. I understand this action cannot be undone.</label>
    </div>
    <div class="form-group row">
  		<div class="col">
  			<a role="button" class="btn btn-secondary" href="/tenant/cancel">Cancel</a>
  			<button type="submit" class="btn btn-danger">Delete User</button>
  		</div>
  	</div>
  </form:form>
</div>
<script src="/webjars/jquery/3.5.1/jquery.min.js"></script>
<script src="/webjars/bootstrap/4.5.0/js/bootstrap.bundle.min.js"></script>
</body>
</html>