# INSTRUCTIONS TO SETUP FORKED API

## STEPS
NOTE: If you communicating between two machines instead of one (vm and host machine for example), ensure the the steps below are completed on ``both`` machines.

## WINDOWS:
1. 
    1.a) If you want to download from my github (this will eventually be taken down), visit <a href=https://github.com/Huntermuze/RaspberryJuice>here</a>.
   
    1.b) Navigate to **/download_here**.
<br></br>
    2.a) If you are reading this, you likely have access to this repo. Hence, navigate to **/modified_api/API** and obtain the files from there.

2. Download the following: ``MC_Py_API.zip``, ``RJ-Fork.jar`` and ``Install_API.bat``.

3. Go to your ``Minecraft Tools`` folder that was provided by the RMIT instructors.

4. Drag/drop and **replace** the current ``MC_Py_API.zip`` file with the newly downloaded one. Do not rename this file.

5. Navigate to ``server/plugins`` (you should be within your ``Minecraft Tools`` directory here).

6. Drag/drop and ``RJ-Fork.jar`` into there and **delete** ``raspberryjuice-1.12.1.jar``.

7. Go back to your ``Minecraft Tools`` folder, and **delete** the existing ``Install_API.bat``. Then **replace** it with the one you just downloaded.

8. Now, run the ``Install_API.bat`` script as usual. Let that complete.

9. Press **'Y'** for Yes upon the first message (it's asking to uninstall the current mcpi version). Then after the second command (installing new API) has executed, press any key to dispose the window.

10. You're good to go! Start your server and your minecraft instance up and test out the ``postToChat()`` function.


## LINUX/MAC:
1. 
    1.a) If you want to download from my github (this will eventually be taken down), visit <a href=https://github.com/Huntermuze/RaspberryJuice>here</a>.
   
    1.b) Navigate to **/download_here**.
<br></br>
    2.a) If you are reading this, you likely have access to this repo. Hence, navigate to **/modified_api/API** and obtain the files from there.

2. Download both ``MC_Py_API.zip`` and ``RJ-Fork.jar``.

3. Go to your ``Minecraft Tools`` folder that was provided by the RMIT instructors.

4. Drag/drop and **replace** the current ``MC_Py_API.zip`` file with the newly downloaded one. Do not rename this file.

5. Navigate to ``server/plugins`` (you should be within your ``Minecraft Tools`` directory here).

6. Drag/drop and ``RJ-Fork.jar`` into there and **delete** ``raspberryjuice-1.12.1.jar``.

7. Open *Terminal* (may need elevated privalleges - use ``sudo`` in that case).

8. Type the following command and hit enter: ``pip uninstall mcpi``. When prompted to proceed, type ``y`` and press enter.

9. Type the following command and hit enter: ``pip install MC_Py_API.zip``, ensuring that this MC_Py_API.zip is the recently downloaded version. This, along with Step 8, emulates the ``Install_API.bat`` file.

10. You're good to go! Start your server and your minecraft instance up and test out the ``postToChat()`` function.