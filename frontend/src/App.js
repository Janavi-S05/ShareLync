import "./App.css";
import React, { useState, useMemo } from "react";
import { useCookies } from "react-cookie";
import { UserAuth } from "./components/UserAuth";
import { Typography, Divider, Container } from "@mui/material";
import { Dashboard } from "./components/Dashboard";

function App() {

  const [cookies] = useCookies(["user"]);

  const user = useMemo(() => {
    if (cookies.user) {
      try {
        return typeof cookies.user === "string"
          ? JSON.parse(cookies.user)
          : cookies.user;
      } catch {
        return null;
      }
    }
    return null;
  }, [cookies.user]);

  const [isLoggedIn, setIsLoggedIn] = useState(!!user);

  return (
    <div className="App" style={{
      margin: 2,
      padding: 6,
      maxWidth: "1200px",
      marginLeft: "auto",
      marginRight: "auto",
      marginTop: "10px",
      borderRadius: "10px",
      boxShadow: "5px 3px 10px 10px lightgray"
    }}>

      <Typography component="h3" variant="h3">
        CloudDrop
      </Typography>

      <Divider />

      {isLoggedIn && user ? (
        <Dashboard user={user} setIsLoggedIn={setIsLoggedIn} />
      ) : (
        <Container
          sx={{
            display: "flex",
            flexDirection: "row",
            justifyContent: "flex-start"
          }}
        >
          <UserAuth setUserLoggedIn={setIsLoggedIn} />
        </Container>
      )}
    </div>
  );
}

export default App;