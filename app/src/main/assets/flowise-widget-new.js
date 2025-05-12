console.log("Dummy widget loaded!");
window.FlowiseChatWidget = {
  init: function() { 
    const d = document.createElement("div");
    d.textContent = "WIDGET WORKS";
    d.style = "font-size:24px;color:green; text-align:center; margin-top:50%;";
    document.getElementById("chat-container").appendChild(d);
  }
};
