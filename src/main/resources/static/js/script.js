$(".toggle").on("click", function () {
  $(".container").stop().addClass("active");
});

$(".close").on("click", function () {
  $(".container").stop().removeClass("active");
});

//传递表单的值
$("#btn").click(function () {
  //将表单的数据提交到toLogin,进行数据库的判断
  $.post("toLogin?",$("#loginForm").serialize(),function(res){
    if (res.flag){
      //如果后端判断成功,则跳转到main页面
      console.log(res);
      location.href = "main";
    } else {
      console.log(res);
      $("#err_msg").html(res.message);
    }
  },"json");
});