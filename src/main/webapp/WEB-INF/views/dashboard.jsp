<%@ page contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<%@ include file="fragments/head.jspf" %>
<c:set var="active" value="dashboard"/>
<!DOCTYPE html>
<html>
<head>
  <title>Dashboard · Digital Steganography</title>
</head>
<body>
<div class="shell">

  <%@ include file="fragments/sidebar.jspf" %>

  <main class="main">

    <!-- Top bar -->
    <div class="top">
      <div>
        <h1>Good day, <c:out value="${sessionScope.user.username}"/>!</h1>
        <p>Your steganography workspace is ready.</p>
      </div>
      <div class="user">
        <span class="badge">● Secure</span>
        <div class="avatar">${fn:toUpperCase(fn:substring(sessionScope.user.username, 0, 1))}</div>
        <c:out value="${sessionScope.user.username}"/>
      </div>
    </div>

    <%@ include file="fragments/flash.jspf" %>

    <!-- 3 action cards -->
    <div class="grid3">
      <div class="card action a1">
        <div class="i">🖼</div>
        <h2>Hide Secret Message</h2>
        <p>Embed your secret message into an image.</p>
        <a class="btn btn-sm" href="${pageContext.request.contextPath}/embed">Go to Embed →</a>
      </div>

      <div class="card action a2">
        <div class="i">📤</div>
        <h2>Extract Secret Message</h2>
        <p>Retrieve hidden messages from an image.</p>
        <a class="btn btn-sm" href="${pageContext.request.contextPath}/extract">Go to Extract →</a>
      </div>

      <div class="card action a3">
        <div class="i">⏱</div>
        <h2>View Operation History</h2>
        <p>Check your past operations and activity.</p>
        <a class="btn btn-sm" href="${pageContext.request.contextPath}/history">View History →</a>
      </div>
    </div>

    <!-- 3 stat cards -->
    <div class="grid3">
      <div class="card stat">
        <small>Total Embeddings</small>
        <b>${empty totalEmbeddings ? 0 : totalEmbeddings}</b>
        <span class="up">↑ ${empty embeddingsThisWeek ? 0 : embeddingsThisWeek} this week</span>
      </div>

      <div class="card stat">
        <small>Total Extractions</small>
        <b>${empty totalExtractions ? 0 : totalExtractions}</b>
        <span class="up">↑ ${empty extractionsThisWeek ? 0 : extractionsThisWeek} this week</span>
      </div>

      <div class="card stat">
        <small>Recent Activity</small>
        <b style="font-size:1.1rem;margin-top:.6rem">Today, ${empty todayOps ? 0 : todayOps} operations</b>
      </div>
    </div>

  </main>
</div>
</body>
</html>