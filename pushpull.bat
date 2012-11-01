@echo off
cls
if not "%1" == "push" if not "%1" == "pull" if not "%1" == "clone" goto error 

hg --config http_proxy.host=ongate3d.ad.onsemi.com:80 --config http_proxy.user=ffyxrr --config http_proxy.passwd=jaRadek7nsemi %1 https://raterwork:Wx9Xe5Za8VQ3@code.google.com/p/chili-to-redmine/

goto exit

:error

echo.
echo.
echo "push" or "pull" is expected as argument"
echo.
echo.

:exit