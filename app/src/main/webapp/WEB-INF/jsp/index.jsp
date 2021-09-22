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
      <div class="jumbotron">
          <h2>AWS SaaS Factory PostgreSQL Row Level Security Demo</h2>
          <p>Use this interface to test the example RLS policies described in the
          <a href="https://aws.amazon.com/blogs/database/multi-tenant-data-isolation-with-postgresql-row-level-security/" target="_blank">Multi-tenant
          data isolation with PostgreSQL Row Level Security</a> blog post.<br/>
          <strong><em>Please note this sample is for demonstration purposes only.</em></strong></p>
          <p>As the SaaS administrator, you can add new tenants to the database. Once you've added a tenant, that tenant can manage their users.<br/>
          Instructions:
          <ol>
          <li>Add at least two tenants to the system as the administrator.</li>
          <li>Add at one or more users to each tenant <em><strong>as that</strong></em> tenant.</li>
          <li>Now try to view or edit users of <em><strong>another</strong></em> tenant and you'll see the RLS policies enforcing isolation of the tenant data.</li>
          </ol>
          </p>
      </div>
    <div class="container pt-3">
      <h3><a href="/admin">Admin Tenant Management</a></h3>
      <p>These actions will be executed as the SaaS administrator.</p>
      </div>
    <div class="container pt-3">
      <h3><a href="/tenant">Tenant User Management</a></h3>
      <p>These actions will be executed in the context of the Tenant you choose to "login" as and will be secured by RLS policies.</p>
  </div>
  <script src="/webjars/jquery/3.5.1/jquery.min.js"></script>
  <script src="/webjars/bootstrap/4.5.0/js/bootstrap.bundle.min.js"></script>
  </body>
</html>