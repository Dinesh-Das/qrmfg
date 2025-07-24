import React, { useEffect, useState } from 'react';
import { Card, Radio, Typography } from 'antd';
// import AdminLayout from '../components/Layout'; // Remove this line

const { Title } = Typography;

const THEME_KEY = 'themeMode';

const getSystemTheme = () => {
  if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
    return 'dark';
  }
  return 'light';
};

const Settings = () => {
  const [mode, setMode] = useState(() => {
    return localStorage.getItem(THEME_KEY) || 'system';
  });

  useEffect(() => {
    let theme = mode;
    if (mode === 'system') {
      theme = getSystemTheme();
    }
    document.body.setAttribute('data-theme', theme);
    localStorage.setItem(THEME_KEY, mode);
  }, [mode]);

  return (
    <Card style={{ maxWidth: 400, margin: '40px auto' }}>
      <Title level={4}>Settings</Title>
      <div style={{ marginBottom: 16 }}>Theme Mode:</div>
      <Radio.Group
        value={mode}
        onChange={e => setMode(e.target.value)}
        buttonStyle="solid"
      >
        <Radio.Button value="system">System</Radio.Button>
        <Radio.Button value="light">Light</Radio.Button>
        <Radio.Button value="dark">Dark</Radio.Button>
      </Radio.Group>
    </Card>
  );
};

export default Settings; 