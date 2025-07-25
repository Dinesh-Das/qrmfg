import React, { useState } from "react";
import { Form, Input, Button, Card, Alert, Typography } from "antd";
import axios from "axios";
import { setToken } from "../utils/auth";
import { Link } from "react-router-dom";
import { notifySuccess, notifyError } from '../utils/notify';

const { Title } = Typography;

const Login = () => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const onFinish = async (values) => {
    setLoading(true);
    setError("");
    console.log("Form submitted!", values); // Debug: see if form submits
    try {
      const response = await axios.post("/qrmfg/api/v1/auth/login", values);
      if (response.data && response.data.token) {
        setToken(response.data.token);
        notifySuccess('Login Successful', 'Welcome!');
        window.location.href = "/qrmfg";
      } else {
        notifyError('Login Failed', 'No token returned');
        setError("Login failed: No token returned");
      }
    } catch (err) {
      notifyError('Login Failed', err.response && err.response.data && err.response.data.message
        ? err.response.data.message
        : "Invalid username or password");
      setError(
        err.response && err.response.data && err.response.data.message
          ? err.response.data.message
          : "Invalid username or password"
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#f0f2f5" }}>
      <Card style={{ width: 350, boxShadow: "0 2px 8px #f0f1f2" }}>
        <Title level={3} style={{ textAlign: "center", marginBottom: 24 }}>Admin Login</Title>
        {error && <Alert message={error} type="error" showIcon style={{ marginBottom: 16 }} />}
        <Form
          layout="vertical"
          onFinish={onFinish}
          autoComplete="off"
          initialValues={{ username: "", password: "" }}
        >
          <Form.Item
            name="username"
            label="Username"
            rules={[{ required: true, message: "Please enter your username" }]}
          >
            <Input size="large" autoFocus placeholder="Enter your username" />
          </Form.Item>
          <Form.Item
            name="password"
            label="Password"
            rules={[{ required: true, message: "Please enter your password" }]}
          >
            <Input.Password size="large" placeholder="Enter your password" />
          </Form.Item>
          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              block
              size="large"
              loading={loading}
            >
              Login
            </Button>
          </Form.Item>
          <div style={{ textAlign: "center" }}>
            <Link to="/qrmfg/forgot-password">Forgot Password?</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
};

export default Login; 