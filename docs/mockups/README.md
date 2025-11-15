# Tresor UI Mockups

This directory contains HTML mockups for all main UI screens in the Tresor application.

## How to View

Simply open any HTML file in your web browser:

```bash
# From the mockups directory
open 01-landing-page.html      # Mac
xdg-open 01-landing-page.html  # Linux
start 01-landing-page.html     # Windows

# Or just double-click the files
```

## Mockup Files

### 1. Landing Page (`01-landing-page.html`)
**What it shows:**
- Hero section with value proposition
- Trust indicators (encryption, blockchain, etc.)
- How It Works section (3-step process)
- Use cases grid
- Pricing preview
- Footer with links

**Key features:**
- Gradient hero background
- Animated progress states
- Responsive pricing cards
- Sticky navigation

---

### 2. Dashboard (`02-dashboard.html`)
**What it shows:**
- Welcome message with user stats
- Key metrics (locked messages, unlocking soon, delivered, total spent)
- Create message button (primary CTA)
- Tab navigation (Pending, Upcoming, Delivered, All)
- Message cards with status and progress

**Key features:**
- Real-time progress bars
- Status indicators (deploying, pending, delivered)
- Hover effects on cards
- Responsive grid layout

---

### 3. Create Message (`03-create-message.html`)
**What it shows:**
- Step 3 of 4: Deployment tier selection
- Progress indicator (75%)
- Four tier options:
  - Budget ($0.50)
  - Standard ($3.00) - Recommended
  - Premium ($13.00)
  - Enterprise ($25.00)

**Key features:**
- Interactive tier card selection
- Feature comparison
- Recommended badge
- Responsive tier cards
- JavaScript for selection state

---

### 4. Message Detail (`04-message-detail.html`)
**What it shows:**
- Message title and emoji
- Countdown timer (28 days)
- Deployment progress (70%)
- Blockchain distribution status
- Technical details (encryption, secret sharing)
- Recipient information
- Action buttons (edit, share, download, cancel)

**Key features:**
- Animated progress bars
- Real-time blockchain status
- Warning messages for destructive actions
- Informational boxes

---

### 5. Settings (`05-settings.html`)
**What it shows:**
- Sidebar navigation (Profile, Notifications, Billing, Security)
- Profile section with avatar and form
- Notification toggles
- Usage statistics
- Payment methods
- Transaction history
- Active sessions
- Security settings (2FA, password change)
- Danger zone (account deletion)

**Key features:**
- Sticky sidebar navigation
- Custom toggle switches
- Stats grid
- Transaction list
- Session management
- Danger zone styling

---

## Design System

All mockups use a consistent design system:

### Colors
- **Primary Purple:** `#6366F1` (Indigo-500)
- **Secondary Purple:** `#8B5CF6` (Violet-500)
- **Success Green:** `#10B981` (Emerald-500)
- **Warning Orange:** `#F59E0B` (Amber-500)
- **Error Red:** `#EF4444` (Red-500)
- **Background:** `#F9FAFB` (Gray-50)
- **Text Primary:** `#111827` (Gray-900)
- **Text Secondary:** `#6B7280` (Gray-500)

### Typography
- **Font Family:** Inter (via Google Fonts)
- **Heading 1:** 4rem (64px) - Hero
- **Heading 2:** 2rem (32px) - Section titles
- **Body:** 1rem (16px)
- **Small:** 0.875rem (14px)

### Spacing
- Uses an 8px grid system
- Common paddings: 1rem (16px), 1.5rem (24px), 2rem (32px)

### Border Radius
- Cards: 16px
- Buttons: 8px
- Inputs: 8px
- Avatars: 50% (circular)

### Shadows
- **Small:** `0 1px 3px rgba(0, 0, 0, 0.1)`
- **Medium:** `0 4px 12px rgba(99, 102, 241, 0.3)`
- **Large:** `0 6px 20px rgba(99, 102, 241, 0.4)`

---

## Responsive Design

All mockups are mobile-responsive with breakpoints at:
- **Desktop:** 1440px+
- **Tablet:** 768px - 1439px
- **Mobile:** < 768px

### Mobile Adaptations
- Stacked layouts instead of grids
- Hamburger menu (where applicable)
- Full-width buttons
- Simplified navigation
- Touch-friendly button sizes (min 44x44px)

---

## Interactive Elements

### Hover States
- Buttons: `translateY(-2px)` with shadow increase
- Cards: Shadow increase
- Links: Color change to primary purple

### Animations
- Progress bars: Pulsing animation
- Pending status: Pulse opacity
- Buttons: Transform and shadow on hover

---

## Browser Compatibility

Tested and works in:
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

---

## Next Steps for Development

When implementing these designs in your application:

1. **Component Library**
   - Extract reusable components (buttons, cards, inputs)
   - Create a component library (React, Vue, etc.)

2. **State Management**
   - Implement real data binding
   - Connect to backend APIs
   - Handle loading and error states

3. **Accessibility**
   - Add proper ARIA labels
   - Ensure keyboard navigation
   - Test with screen readers
   - Verify color contrast ratios (WCAG AA)

4. **Performance**
   - Optimize images
   - Lazy load content
   - Minimize JavaScript
   - Use CSS variables for theming

5. **Testing**
   - Unit tests for components
   - Integration tests for user flows
   - E2E tests for critical paths

---

## Design Tools Integration

### Figma
These HTML mockups can serve as references for creating high-fidelity designs in Figma:

1. Use the **exact colors and spacing** specified above
2. Create reusable **components** (buttons, cards, inputs)
3. Set up **text styles** for all typography variants
4. Build **auto-layout** frames for responsive design
5. Create **interactive prototypes** with Smart Animate

### Export to Code
These mockups are already production-ready HTML/CSS that can be:
- Used as-is for rapid prototyping
- Extracted into React/Vue/Svelte components
- Converted to Tailwind CSS classes
- Adapted for your framework of choice

---

## Feedback & Iteration

As you review these mockups, consider:

1. **User Flow:** Does the flow make sense?
2. **Information Hierarchy:** Is the most important info prominent?
3. **Visual Appeal:** Does it look modern and trustworthy?
4. **Accessibility:** Can all users interact with it?
5. **Brand Alignment:** Does it match your vision for Tresor?

---

## License

These mockups are part of the Tresor project and share the same license.

---

**Created:** November 2024
**Last Updated:** November 2024
**Status:** ✅ Complete - Ready for review
