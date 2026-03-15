import React, { useEffect, useState, useMemo } from "react";
import { Box, Button, TextField, Container, Avatar, Tooltip, Alert } from "@mui/material";
import DocumentsImg from '../documents.jpg';
import { useCookies } from "react-cookie";
import { RegistrationForm } from "../RegistrationForm";
import { useGoogleLogin } from "@react-oauth/google";
import { googleLogout } from '@react-oauth/google';
import GoogleIcon from '@mui/icons-material/Google';

export const UserAuth = (props) => {
    const { userLoggedIn, setUserLoggedIn } = props;
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [loginError, setLoginError] = useState(false);
    const [cookies, setCookie, removeCookie] = useCookies(['user']);
    const [showRegForm, setShowRegForm] = useState(false);
    const [profileImage, setProfileImage] = useState("");

    const user = useMemo(() => {
        if (cookies.user) {
            try {
                return typeof cookies.user === 'string' ? JSON.parse(cookies.user) : cookies.user;
            } catch (e) { return null; }
        }
        return null;
    }, [cookies.user]);

    useEffect(() => {
        if (userLoggedIn && user) {
            if (user.google && user.google.pictureLink) {
                setProfileImage(user.google.pictureLink);
            }
        }
    }, [user, userLoggedIn]);

    const handleLoginSuccess = (userObject) => {
        setCookie("user", userObject, { path: "/" });
        setUserLoggedIn(true);
    };

    const fetchUserGoogleProfile = async (token) => {
        const url = `https://www.googleapis.com/oauth2/v1/userinfo?access_token=${token}`;
        try {
            const request = await fetch(url);
            const data = await request.json();
            const newUserInfo = { 
                google: { token, email: data.email, pictureLink: data.picture, name: data.name },
                username: data.email, name: data.name 
            };
            const backendPost = "/api/auth/login/google";
            const bePost = await fetch(backendPost, {
                method: "POST", headers: { "Content-Type": "application/json" },
                body: JSON.stringify(newUserInfo.google)
            });
            const beData = await bePost.json();
            newUserInfo.token = beData.data.accessToken;
            handleLoginSuccess(newUserInfo);
        } catch (error) { console.error("Google login failed", error); }
    }

    const glogin = useGoogleLogin({
        onSuccess: tokenResponse => fetchUserGoogleProfile(tokenResponse.access_token),
        onError: (e) => console.error(e)
    });

    const login = async () => {
        const url = `/api/auth/login`;
        const resp = await fetch(url, {
            method: "POST", headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ "username": username, "password": password })
        });
        const data = await resp.json();
        if (data.isError) {
            setLoginError(true);
        } else {
            const userObj = {
                name: data.data.name, username: username,
                token: data.data.access_token
            };
            handleLoginSuccess(userObj);
        }
    }

    const logout = () => {
        removeCookie("user", { path: "/" });
        setUserLoggedIn(false);
        googleLogout();
    }

    // Renders the logged-in view (Avatar and Logout button)
    if (userLoggedIn && user) {
        return (
            <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center" }} id="avatar" gap={1} p={1}>
                <Tooltip title={user.name}>
                    <Avatar src={profileImage} alt={user.username} sx={{ width: 64, height: 64 }} />
                </Tooltip>
                <Button size="small" variant="contained" data-test="logout" onClick={logout}>
                    Logout
                </Button>
            </Box>
        );
    }
    
    // Renders the login/signup form view
    return (
        <Container>
            <Box sx={{ borderRadius: 2, marginTop: 1 }}>
                <img className="RoundedImage" src={DocumentsImg} width="300" height="300" alt="documents"/>
            </Box>
            {showRegForm ? <RegistrationForm closeForm={() => setShowRegForm(false)} /> : (
                <Box component="form" onSubmit={(e) => e.preventDefault()} sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
                    <TextField margin="normal" required id="email" label="email" autoComplete="email" autoFocus onChange={(e) => setUsername(e.target.value)} />
                    <TextField margin="normal" required id="password" label="password" type="password" onChange={(e) => { setPassword(e.target.value); setLoginError(false); }} />
                    {loginError && <Box p={1}><Alert variant="outlined" severity="error">Error!!! Are you sure you entered the right username and password?</Alert></Box>}
                    <Box gap={1} sx={{ display: "flex", flexDirection: "row" }}>
                        <Button size="small" variant="contained" type="button" onClick={login}>Login</Button>
                        <Button size="small" variant="contained" type="button" onClick={() => setShowRegForm(true)}>New Account?</Button>
                    </Box>
                    <Box p={1} m={1}>
                        <Button variant="contained" onClick={() => glogin()} startIcon={<GoogleIcon />}>Google Login</Button>
                    </Box>
                </Box>
            )}
        </Container>
    );
}