import React, { useState } from "react";
import { Form, Input, Button, Card, Alert, Typography } from "antd";
import axios from "axios";
import { useSearchParams, useNavigate } from "react-router-dom";

const { Title } = Typography;

const ResetPassword = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const token = searchParams.get("token");

  const onFinish = async (values) => {
    setLoading(true);
    setError("");
    setSuccess("");
    try {
      const response = await axios.post("/api/v1/auth/reset-password", {
        token,
        newPassword: values.newPassword,
      });
      setSuccess("Password reset successful! Redirecting to login...");
      setTimeout(() => navigate("/login"), 2000);
    } catch (err) {
      setError(
        err.response && err.response.data
          ? err.response.data
          : "Failed to reset password."
      );
    } finally {
      setLoading(false);
    }
  };

  if (!token) {
    return (
      <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#f0f2f5" }}>
        <Card style={{ width: 350, boxShadow: "0 2px 8px #f0f1f2" }}>
          <Alert message="Invalid or missing reset token." type="error" showIcon />
        </Card>
      </div>
    );
  }

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#f0f2f5" }}>
      <Card style={{ width: 350, boxShadow: "0 2px 8px #f0f1f2" }}>
        <Title level={3} style={{ textAlign: "center", marginBottom: 24 }}>Reset Password</Title>
        {success && <Alert message={success} type="success" showIcon style={{ marginBottom: 16 }} />}
        {error && <Alert message={error} type="error" showIcon style={{ marginBottom: 16 }} />}
        <Form layout="vertical" onFinish={onFinish} autoComplete="off">
          <Form.Item
            name="newPassword"
            label="New Password"
            rules={[{ required: true, message: "Please enter your new password" }, { min: 6, message: "Password must be at least 6 characters" }]}
          >
            <Input.Password size="large" placeholder="Enter new password" />
          </Form.Item>
          <Form.Item
            name="confirmPassword"
            label="Confirm Password"
            dependencies={["newPassword"]}
            rules={[
              { required: true, message: "Please confirm your new password" },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue("newPassword") === value) {
                    return Promise.resolve();
                  }
                  return Promise.reject(new Error("Passwords do not match!"));
                },
              }),
            ]}
          >
            <Input.Password size="large" placeholder="Confirm new password" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" block size="large" loading={loading}>
              Reset Password
            </Button>
          </Form.Item>
        </Form>
      </Card>
    </div>
  );
};

export default ResetPassword; 