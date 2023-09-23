import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import Home from './pages/Home';
import Event from './pages/Event';
import Checkout from './pages/Checkout';

export default function App() {
  return (
    <BrowserRouter>
      <Toaster position="top-right" />
      <div className="min-h-screen bg-white text-gray-900">
        <nav className="bg-[#dc3558] px-6 py-3">
          <a href="/" className="text-xl font-bold text-white">🎟️ TicketMaster</a>
        </nav>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/event/:eventId" element={<Event />} />
          <Route path="/checkout/:bookingId" element={<Checkout />} />
        </Routes>
      </div>
    </BrowserRouter>
  );
}