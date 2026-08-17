import { useEffect, useState } from 'react';

interface HealthResponse {
  status: string;
  service: string;
  version: string;
}

function App() {
  const [health, setHealth] = useState<HealthResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch('/health')
      .then((res) => res.json())
      .then(setHealth)
      .catch((err) => setError(err.message));
  }, []);

  return (
    <div className="min-h-screen flex items-center justify-center">
      <div className="bg-white rounded-2xl shadow-lg p-8 max-w-md w-full">
        <h1 className="text-2xl font-bold mb-4">SERVICE_NAME</h1>

        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
            ❌ Error: {error}
          </div>
        )}

        {health && (
          <div className="bg-green-50 border border-green-200 rounded-lg p-4">
            <p className="text-green-700 font-semibold">
              ✅ Status: {health.status}
            </p>
            <p className="text-green-600 text-sm mt-1">
              Service: {health.service}
            </p>
          </div>
        )}

        {!health && !error && (
          <div className="text-gray-400 text-center py-8">
            ⏳ Connecting...
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
