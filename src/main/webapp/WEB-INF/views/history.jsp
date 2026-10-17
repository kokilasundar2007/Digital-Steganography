<%@ page contentType="text/html;charset=UTF-8" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="active" value="history"/>
<!DOCTYPE html>
<html>
<head>
  <title>History · Digital Steganography</title>
</head>
<body>
<div class="shell">

  <%@ include file="fragments/sidebar.jspf" %>

  <main class="main">

    <div class="top">
      <div>
        <h1>Operation History</h1>
        <p>View your past steganography operations.</p>
      </div>
    </div>

    <%@ include file="fragments/flash.jspf" %>

    <!-- Search and filters -->
    <form class="filters" method="get" action="${pageContext.request.contextPath}/history">
      <input class="search" name="q" placeholder="🔍 Search by image name..."
             value="<c:out value='${param.q}'/>">

      <select class="select" name="type">
        <option value="">All Operations</option>
        <option value="EMBED"   ${param.type == 'EMBED'   ? 'selected' : ''}>Embed</option>
        <option value="EXTRACT" ${param.type == 'EXTRACT' ? 'selected' : ''}>Extract</option>
      </select>

      <input class="select" type="date" name="date" value="<c:out value='${param.date}'/>">

      <button class="btn btn-sm">Filter</button>
    </form>

    <div class="card">
      <c:choose>

        <%-- Records found: show table --%>
        <c:when test="${not empty records}">
          <div style="overflow-x:auto">
            <table>
              <tr>
                <th>#</th>
                <th>Operation Type</th>
                <th>Image Filename</th>
                <th>Date &amp; Time</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>

              <c:forEach var="r" items="${records}" varStatus="s">
                <tr>
                  <td>${s.count + (currentPage - 1) * pageSize}</td>
                  <td><c:out value="${r.operationType}"/></td>
                  <td><c:out value="${r.filename}"/></td>
                  <td><c:out value="${r.createdAt}"/></td>
                  <td>
                    <span class="pill ${r.success ? '' : 'fail'}">${r.success ? 'Success' : 'Failed'}</span>
                  </td>
                  <td>
                    <a class="btn btn-sm" href="${pageContext.request.contextPath}/history?view=${r.id}">View</a>
                  </td>
                </tr>
              </c:forEach>
            </table>
          </div>

          <!-- Page numbers -->
          <div class="pager">
            <c:forEach begin="1" end="${totalPages}" var="p">
              <a class="${p == currentPage ? 'on' : ''}"
                 href="?page=${p}&q=${param.q}&type=${param.type}&date=${param.date}">${p}</a>
            </c:forEach>
          </div>
        </c:when>

        <%-- No records --%>
        <c:otherwise>
          <div class="empty-state">
            <div style="font-size:2rem">🗂</div>
            <b>No history found</b>
            <p>Your operations will appear here once you start using the application.</p>
          </div>
        </c:otherwise>

      </c:choose>
    </div>

  </main>
</div>
</body>
</html>