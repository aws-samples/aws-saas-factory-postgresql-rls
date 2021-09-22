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
  <div class="container pt-3">
    <h3>Login to Tenant Management</h3>
    <p>Choose the tenant you'd like to "authenticate" as to mange tenant users.</p>
    <form action="/login" method="post">
    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
    <input type="hidden" name="password" id="password"/>
    <input type="hidden" name="username" id="username"/>
    <div class="form-group row">
    <div class="col">
    <select name="tenant" id="tenant" class="form-control">
    <option value="">Select tenant...</option>
    <c:forEach items="${tenants}" var="tenant">
      <option value="${tenant.id}">${tenant.name}</option>
    </c:forEach>
    </select>
    </div>
    </div>
    <div class="form-group row">
    	  <div class="col">
    	    <a role="button" class="btn btn-secondary" href="/">Cancel</a>
    	    <button type="submit" class="btn btn-primary">Submit</button>
    	  </div>
    	</div>
    </form>
  </div>
  <script src="/webjars/jquery/3.5.1/jquery.min.js"></script>
  <script src="/webjars/bootstrap/4.5.0/js/bootstrap.bundle.min.js"></script>
  <script type="text/javascript">
  $(document).ready(function($) {
      $("#tenant").change(function(e) {
        var selectedValue = $(this).val();
        var selectedText = $(this).find("option:selected").text();
        $("#password").val(selectedValue);
        $("#username").val(selectedText);
      });
    });
  </script>
  </body>
</html>