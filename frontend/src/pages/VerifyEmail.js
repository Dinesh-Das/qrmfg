import React, { useEffect, useState } from "react";
import { Card, Alert, Typography, Spin, Button } from "antd";
import axios from "axios";
import { useSearchParams, useNavigate } from "react-router-dom";

const { Title } = Typography;

const VerifyEmail = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");
  const token = searchParams.get("token");

  useEffect(() => {
    const verify = async () => {
      if (!token) {
        setError("Invalid or missing verification token.");
        setLoading(false);
        return;
      }
      try {
        const response = await axios.get(`/api/v1/auth/verify-email?token=${token}`);
        setSuccess("Email verified successfully! You can now log in.");
      } catch (err) {
        setError(
          err.response && err.response.data
            ? err.response.data
            : "Failed to verify email."
        );
      } finally {
        setLoading(false);
      }
    };
    verify();
  }, [token]);

  return (
    <div style={{ minHeight: "100vh", display: "flex", alignItems: "center", justifyContent: "center", background: "#f0f2f5" }}>
      <Card style={{ width: 400, boxShadow: "0 2px 8px #f0f1f2" }}>
        <Title level={3} style={{ textAlign: "center", marginBottom: 24 }}>Email Verification</Title>
        {loading && <Spin tip="Verifying..." style={{ width: "100%" }} />}
        {!loading && success && <Alert message={success} type="success" showIcon style={{ marginBottom: 16 }} />}
        {!loading && error && <Alert message={error} type="error" showIcon style={{ marginBottom: 16 }} />}
        {!loading && (
          <Button type="primary" block onClick={() => navigate("/login")}>Go to Login</Button>
        )}
      </Card>
    </div>
  );
};

export default VerifyEmail; 