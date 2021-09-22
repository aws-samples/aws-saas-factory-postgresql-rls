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
<%@ taglib prefix="security" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html lang="en">
<head>
	<title><c:choose><c:when test="${not empty user.id}">Edit User</c:when><c:otherwise>Add User</c:otherwise></c:choose></title>
	<link href="/webjars/bootstrap/4.5.0/css/bootstrap.min.css" rel="stylesheet">
</head>
<body>
  <nav aria-label="breadcrumb">
    <ol class="breadcrumb">
      <li class="breadcrumb-item"><a href="/">Home</a></li>
      <li class="breadcrumb-item"><a href="/tenant">User Manager</a></li>
      <li class="breadcrumb-item active" aria-current="page">Edit User</li>
    </ol>
  </nav>
<div class="container pt-3">
  <div class="row">
    <div class="col">
      <h2><c:choose><c:when test="${not empty user.id}">Edit User</c:when><c:otherwise>Add User</c:otherwise></c:choose></h2>
    </div>
  </div>
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
	<form:form modelAttribute="user" method="post" action="/tenant/editUser">
	<form:hidden path="id"/>
	<form:hidden path="tenant"/>
	<div class="form-group row">
    	  <spring:bind path="email">
    	    <div class="col-2">
    	      <form:label path="email" for="email" class="col-form-label">Email</form:label>
    	    </div>
    	    <div class="col-10">
    	      <form:input path="email" type="text" class="form-control ${status.error ? 'is-invalid' : ''}" style="width:auto" placeholder="Email" />
    	      <form:errors path="email" class="invalid-feedback" />
    	    </div>
    	  </spring:bind>
    	</div>
	<div class="form-group row">
        	  <spring:bind path="givenName">
        	    <div class="col-2">
        	      <form:label path="givenName" for="givenName" class="col-form-label">First Name</form:label>
        	    </div>
        	    <div class="col-10">
        	      <form:input path="givenName" type="text" class="form-control ${status.error ? 'is-invalid' : ''}" style="width:auto" placeholder="First Name" />
        	      <form:errors path="givenName" class="invalid-feedback" />
        	    </div>
        	  </spring:bind>
        	</div>
	<div class="form-group row">
            	  <spring:bind path="familyName">
            	    <div class="col-2">
            	      <form:label path="familyName" for="familyName" class="col-form-label">Last Name</form:label>
            	    </div>
            	    <div class="col-10">
            	      <form:input path="familyName" type="text" class="form-control ${status.error ? 'is-invalid' : ''}" style="width:auto" placeholder="Last Name" />
            	      <form:errors path="familyName" class="invalid-feedback" />
            	    </div>
            	  </spring:bind>
            	</div>
	<div class="form-group row">
	  <div class="col-10 offset-2">
	    <a role="button" class="btn btn-secondary" href="/tenant/cancel">Cancel</a>
	    <button type="submit" class="btn btn-primary">Submit</button>
	  </div>
	</div>
	</form:form>
</div>
  <script src="/webjars/jquery/3.5.1/jquery.min.js"></script>
  <script src="/webjars/bootstrap/4.5.0/js/bootstrap.bundle.min.js"></script>
</body>
</html>